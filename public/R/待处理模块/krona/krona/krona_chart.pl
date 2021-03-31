#!/usr/bin/perl -w

use strict;
use warnings;
use Getopt::Long;
use FindBin qw($Bin);
use File::Copy;
use Scalar::Util qw(looks_like_number);

my %opts;
GetOptions( \%opts,"i=s","o=s", "n=s", "c=s");
my $usage = <<"USAGE";

       Usage :perl $0 [options]         
                        -i      * input otu_table file .
                        -o      * output file.
                        -n      Name of the highest level., default is root.
                        ##-c      y/n, contain zero value. default y
USAGE

die $usage if( !($opts{i} && $opts{o}) );

my $input_file = $opts{i};
my $output_file = $opts{o};
my $name = defined($opts{n}) ? $opts{n} : "root";
my $c = defined($opts{c}) ? $opts{c} : "y";

my $ImportText = "$Bin/ImportText.pl";
my $perl_bin = $^X;

open IN,"$opts{i}";
my $header = <IN>;
chomp $header;
my @s = split /\t/, $header;
my $col_num = @s;
my $row_num = 2;
while (<IN>){
        chomp;
        @s = split /\t/, $_;
        die "Error: col number in row $row_num is not equal to header\n" if @s != $col_num;
        for (my $i = 1; $i <= $#s; $i++){
                my $col = $i + 1;
                die "Error: element in row $row_num col $col is not numeric\n" unless isnumeric( $s[$i] );
        }
        $row_num++;
}
close IN;


open IN,"$input_file";
my @data;
while (<IN>){
	chomp;
	my @s = split /\t/, $_;
	push @data, [@s];
}
for(my $i = 1; $i <= $#data; $i++){
	my @split_tax = split /;/, $data[$i][0];
	my @tax_inf = trim_whitespace(\@split_tax );
	$data[$i][0] = join "\t", @tax_inf;
}

my @sample_files;
my @sample_names;
for(my $j = 1; $j < $col_num; $j++){
	my $sample_file = "$data[0][$j].txt";
	push @sample_files, $sample_file;
	push @sample_names, $data[0][$j];
	open OUT, ">$sample_file";
	for(my $i = 1; $i <= $#data; $i++){
		if ( $c eq "y" ){
			print OUT "$data[$i][$j]\t$data[$i][0]\n";
		} else{
			if ( $data[$i][$j] > 0 ){
				print OUT "$data[$i][$j]\t$data[$i][0]\n";
			}
		}
	}
	close OUT;
}


my $cmd = "\"$perl_bin\" \"$ImportText\"";
for (my $i = 0; $i <= $#sample_files; $i++){
	$cmd .= " $sample_files[$i],$sample_names[$i]"
}
$cmd .= " -n \"$name\" -o krona.html";

my $ret = system($cmd);
if ( $ret ) {
        die "Error, died with $ret";
}

foreach( @sample_files ){
	unlink $_;
}

move('krona.html', $output_file);

sub trim_whitespace{
        my $array = shift;
        my @array = @{$array};
        foreach ( @array ){
                $_=~s/^\s+//;
                $_=~s/\s+$//;
		$_=~s/^.__//;
        }
        return @array;
}


sub isnumeric {  
        my $val = shift;  
        looks_like_number $val;
}  

