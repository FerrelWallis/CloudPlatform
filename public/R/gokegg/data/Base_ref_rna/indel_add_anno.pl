#!usr/bin/perl -w 

use strict;

die "usage:perl $0 <snp or indel all.anno> <exon.anno> \n" unless @ARGV == 2;

open A,$ARGV[0];    open B,$ARGV[1];   

my %all;    my %exon;   my %anno;
print "Chr\tpos\tpos\tref\talt\tclassify\tdetail\tregion\tgene\ttype\n";
while(<B>){
    chomp;my @a=split /\t+/;
    $exon{$a[3]}{$a[4]}=[@a];
}
close B;

while(<A>){
    chomp;my @a=split /\t+/;   
    my @b=split /,/,$a[1];
    if(exists $exon{$a[2]}{$a[3]}){
        my @d=split /:/,$exon{$a[2]}{$a[3]}[2];
        my @e=split //,$d[-1];  my @f=split //,$d[-2];
        print "$a[2]\t$a[3]\t$a[3]\t$a[5]\t$a[6]\t$a[-2]\t$a[-1]\t$a[0]\t$a[1]\t$exon{$a[2]}{$a[3]}[1]\n";
    }
    else{
        print "$a[2]\t$a[3]\t$a[3]\t$a[5]\t$a[6]\t$a[-2]\t$a[-1]\t$a[0]\t$a[1]\t_\n";
    }
}
close A;