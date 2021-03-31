#!/usr/bin/perl
use strict;
use warnings;
use Getopt::Long;
use FindBin qw($Bin);
use File::Path;
use File::Copy;
use Scalar::Util qw(looks_like_number);

my $r_path = "$Bin/../../bin/R-3.1.0_x64/bin/x64";
my $bugbase_path = "$Bin/BugBase-v.0.1.0";
$ENV{'PATH'} = "$r_path;$ENV{'PATH'}";
$ENV{'BUGBASE_PATH'} = "$bugbase_path";


my %opts;
GetOptions( \%opts, "i=s",  "log2=s", "otus=s", "prediction=s", "contribution=s", "threshold=s",
);
my $usage = <<"USAGE";

    Usage: Error

USAGE

die $usage unless defined($opts{i});

check_data($opts{i});

my $log2 = "";
if( $opts{log2} eq "yes"){
    $log2 = "-l";
}
my $threshold = "";
if ( defined($opts{threshold}) ) {
    $threshold = "-T $opts{threshold}";
}


my $script = "$Bin/BugBase-v.0.1.0/bin/run.bugbase.r";
my $cmd = "Rscript \"$script\" -i \"$opts{i}\" -o out_dir  $log2 $threshold -x";
my $ret = system($cmd);
if ( $ret ) {
        die "Error, died with $ret";
}

getRTable_v2('out_dir/normalized_otus/16s_normalized_otus.txt' , $opts{otus});
getRTable_v2('out_dir/otu_contributions/contributing_otus.txt' , $opts{contribution});
getRTable_v2('out_dir/predicted_phenotypes/predictions.txt' , $opts{prediction});


rmtree( 'out_dir' );


# getRTable(in, out)
sub getRTable_v2{
    my $in = shift;
    my $out = shift;
    open IN,"$in";
    open OUT,">$out";
    my $h = <IN>;
    chomp $h;
    my @s = split /\t/, $h;
    $s[0] = "OTU ID";
    $h = join "\t", @s;
    print OUT "$h\n";
    while (<IN>){
          chomp;
          print OUT "$_\n";
    }
    close IN;
    close OUT;
}



# getRTable(in, out)
sub getRTable{
    my $in = shift;
    my $out = shift;
    open IN,"$in";
    open OUT,">$out";
    my $h = <IN>;
    print OUT "ID\t$h";
    while (<IN>){
          print OUT "$_";
    }
    close IN;
    close OUT;
}


sub deal_func_table {
    my $biom_table = shift;
    my $output = shift;
    open IN,"$biom_table";
    open OUT,">$output";
    while(<IN>){
        chomp;
        my @s = split /\t/, $_;
        pop @s;
        my $line = join "\t", @s;
        print OUT "$line\n";
    }
    close IN;
    close OUT;
}


sub deal_biom_table{
    my $biom_table = shift;
    my $output = shift;
    open IN,"$biom_table";
    open OUT,">$output";
    while(<IN>){
        chomp;
        unless($_=~/^\s*#/){
            print OUT "$_\n";
        }
        
    }
    close IN;
    close OUT;
}


use JSON;
# biom table
sub biom2table{
    my $biom_file = shift;
    my $table_file = shift;
    my $text;
    {
	    open FH,"$biom_file" or die $!;
            local $/=undef;
            $text = <FH>;
	    close FH;
    }

    my $json = new JSON;
    my $biom = $json->decode("$text");
    my $table = "";


    my @columns = @{$biom->{'columns'}};
    my @col_labs;
    for (my $i = 0; $i <= $#columns; $i++){
	    push @col_labs, $columns[$i]->{'id'};
    }
    my $header = join "\t", ("OTU ID", @col_labs);
    $table .= "$header\n";


    my @rows = @{$biom->{'rows'}};
    my @row_labs;
    for (my $i = 0; $i <= $#rows; $i++){
	    push @row_labs, $rows[$i]->{'id'};
    }

    my @data_array = @{$biom->{'data'}};
    my %data;
    foreach ( @data_array ){
	    $data{$_->[0]}{$_->[1]} = $_->[2];
    }

    my $row_num = $biom->{'shape'}->[0];
    my $col_num = $biom->{'shape'}->[1];

    for (my $x = 0; $x < $row_num; $x++){
	    my @row_data_array;
	    for(my $y = 0; $y < $col_num; $y++){
		    if ( defined( $data{$x}{$y} ) ){
	    		push @row_data_array, $data{$x}{$y};
		    } else {
	    		push @row_data_array, '0';
	    	}
	    }
	    my $line = join "\t", ( $row_labs[$x], @row_data_array );
	    $table .= "$line\n";
    }


    open OUT, ">$table_file";
    print OUT "$table";
}


# table biom tax
sub table2biom{
    my $table_file = shift;
    my $biom_file = shift;
    my $tax_file = shift;
    my %biom; ## data structure

    ## init
    $biom{'generated_by'} = "QIIME 1.9.0";
    $biom{'matrix_type'} = "sparse";
    $biom{'date'} = "2015-04-30T14:44:47.593850";
    $biom{'matrix_element_type'} = "float";
    $biom{'format_url'} = "http://biom-format.org";
    $biom{'format'} = "Biological Observation Matrix 2.1.0";
    $biom{'id'} = "None";
    $biom{'type'} = undef;

    open IN,"$table_file";
    my $header = <IN>;
    chomp $header;
    my @col_lab = split /\t/, $header;
    shift @col_lab;
    my $col_num = @col_lab;

    my @row_lab;
    my $row_num = 0;

    my @data;
    while (<IN>){
	    $row_num ++;
    	chomp;
        my @s = split /\t/, $_;
        my $row_lab = shift @s;
	    push @row_lab, $row_lab;
        die "Error: col number in row $row_num is not equal to header\n" if @s != $col_num;
        for (my $i = 0; $i <= $#s; $i++){
                my $col = $i + 1;
                die "Error: element in row $row_num col $col is not numeric\n" unless isnumeric( $s[$i] );
        }

	    my $x = $row_num - 1;
	    for (my $i = 0; $i <= $#s; $i++){
		    my $y = $i;
		    if ( $s[$i] > 0 ){
			    push @data,[$x, $y, "$s[$i]"];
		    }
	    }
    }
    close IN;

    $biom{'shape'} = [$row_num, $col_num];
    $biom{'data'} = \@data;

    my @columns;
    for(my $i = 0; $i <= $#col_lab; $i++){
	    push @columns, { 'metadata' => undef, 'id' => "$col_lab[$i]"  };
    }
    $biom{'columns'} = \@columns;

    my @rows;
    if ( $tax_file ){
	    open IN,"$tax_file";
	    my %hs;
	    while (<IN>){
		    chomp;
		    my @s = split /\t/, $_;
		    my @split_tax = split /;/, $s[1];
		    my @tax_inf = trim_whitespace(\@split_tax );
		    #print "@tax_inf\n";
		    $hs{$s[0]} = \@tax_inf;
	    }
	    close IN;

	    for(my $i = 0; $i <= $#row_lab; $i++){
		    my %md;
		    if ( defined( $hs{$row_lab[$i]} ) ){
			    $md{'taxonomy'} = $hs{$row_lab[$i]};
			    push @rows, { 'metadata' => \%md, 'id' => "$row_lab[$i]"  };
		    } else {
			    die "Error: $row_lab[$i] can not found in mapping file\n";
		    }
	    }
    } else { 
	    for(my $i = 0; $i <= $#row_lab; $i++){
		    push @rows, { 'metadata' => undef, 'id' => "$row_lab[$i]"  };
	    }
    }
    $biom{'rows'} = \@rows;

    #print  Dumper(\%biom);
    my $json = encode_json \%biom;

    open OUT,">$biom_file";
    print OUT "$json\n";

}






sub trim_whitespace{
	my $array = shift;
	my @array = @{$array};
	foreach ( @array ){
		$_=~s/^\s+//;
		$_=~s/\s+$//;
	}
	return @array;
}



##check matrix and sample group data

sub check_data{
	my $matrix = shift;
	my $sample_group = shift;
	open IN,"$matrix";
	my $header = <IN>;
	chomp $header;
	my @s = split /\t/, $header;
        my %hs; 
        foreach ( @s ){ 
                $hs{$_} = 1;
        }
	my $col_num = @s;
	my $row_num = 2;
	while (<IN>){
        	chomp;
	        my @s = split /\t/, $_;
	        die "Error: col number in row $row_num is not equal to header!\n" if @s != $col_num;
	        for (my $i = 1; $i <= $#s; $i++){
	                my $col = $i + 1;
	                die "Error: element in row $row_num col $col is not numeric!\n" unless isnumeric( $s[$i] );
	        }
	        $row_num++;
	}
	close IN;
	if ( defined $sample_group ){
		my %cnt;
	        open IN,"$sample_group";
	        while (<IN>){
			chomp;
        	        my @s = split /\t/, $_;
			die "Error: sample group file each line must be 2 columns!\n" if @s != 2;
	                $cnt{$s[0]} += 1;
	                die "Error: sample name: $s[0] in sample group file is not exist in the matrix file!\n" unless defined($hs{$s[0]});
	        }
	        close IN;

		foreach ( keys %cnt){
		        if ( $cnt{$_} > 1){
		                die "Error: sample name: $_ is not uniq in the sample group file!\n";
		        }
		}
	}
}
use Scalar::Util qw(looks_like_number);
sub isnumeric {  
        my $val = shift;  
        looks_like_number $val;
}  


# in out col_num
sub filter_chars{
    my $in = shift;
    my $out = shift;
    my $num = shift;
    unless(  defined($num) ){
        $num = 0;
    }
    my @chars = ( "\'", "\"" );
    open IN,"$in";
    open OUT,">$out";
    while(<IN>){
        chomp;
        my @s = split /\t/, $_;
        foreach(@chars){
            #print OUT "$_\n";
            $s[$num]=~s/$_//g;
        }
        my $line = join "\t", @s;
        print OUT "$line\n";
    }
    close IN;
    close OUT;
}