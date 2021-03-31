#!/usr/bin/perl -w

use strict;
use warnings;
use Getopt::Long;

use File::Copy;
use File::Basename;
use File::Path;

use FindBin qw($Bin);
use Scalar::Util qw(looks_like_number);

my $r_path = "$Bin/../../bin/R-3.1.0_x64/bin/x64";
$ENV{'PATH'} = "$r_path;$ENV{'PATH'}";

my %opts;
GetOptions( \%opts,"i=s","o=s", "t=s", "r=s", "fctProfiling=s", "refProfile=s", "shortReadMode=s" );
my $usage = <<"USAGE";
       Usage :perl $0 [options]         
                        -i       input otu_table file
                        -o       output 
                        -t       tax assign 
                        -r       reference database
USAGE

die $usage if( !($opts{i} && $opts{o}) );

open IN,"$opts{i}";
my $header = <IN>;
chomp $header;
my @s = split /\t/, $header;
my @samples = @s;
shift @samples;
my $col_num = @s;
my $row_num = 2;
while (<IN>){
        chomp;
        @s = split /\t/, $_;
        die "col number in row $row_num is not equal to header\n" if @s != $col_num;
        for (my $i = 1; $i <= $#s; $i++){
                my $col = $i + 1;
                die "element in row $row_num col $col is not numeric\n" unless isnumeric( $s[$i] );
        }
        $row_num++;
}
close IN;

sub isnumeric {  
        my $val = shift;  
        looks_like_number $val;
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

open IN,"$opts{t}";
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


open OUT,">otu_tax_table.txt";
open IN,"$opts{i}";
$header = <IN>;
chomp $header;
@s = split /\t/, $header;
$s[0] = "OTU ID";
$header = join "\t", @s;
print OUT "# Constructed from biom file\n";
print OUT "#$header\ttaxonomy\n";
while (<IN>){
        chomp;
	my @s = split /\t/, $_;
	if ( defined( $hs{$s[0]} ) ){
		my @lines = @{$hs{$s[0]}};
        foreach (@lines) {
            $_=~s/^.__//;
        }
		my $line = join "; ", @lines;
		print OUT "$_\t$line\n";
	} else {
		die "Error: OTU: $s[0] do not contain tax info in tax assign file\n";
	}
}
close IN;
close OUT;


my $db = "$Bin/../../db/$opts{r}";

open RCMD,">cmd.r";
print RCMD <<"RSCRIPT";

library(Tax4Fun)
QIIMESingleData <- importQIIMEData("otu_tax_table.txt")
Tax4FunOutput <- Tax4Fun(QIIMESingleData, "$db", fctProfiling = $opts{fctProfiling}, refProfile = "$opts{refProfile}", shortReadMode = $opts{shortReadMode}, normCopyNo = TRUE) 
KO_table = t(Tax4FunOutput\$Tax4FunProfile)
write.table(KO_table, file="KO_table.txt", quote = FALSE, sep="\t", eol = "\n", na = "NA", dec = ".", row.names = TRUE,col.names = TRUE)

RSCRIPT
close RCMD;

my $ret = system("R --restore --no-save < cmd.r");
if ( $ret ) {
        die "Error, died with $ret";
}


getRTable("KO_table.txt", 'tmpout');
filter_chars('tmpout', $opts{o});
unlink 'cmd.r';
unlink 'otu_tax_table.txt';
unlink 'KO_table.txt';
unlink 'tmpout';

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